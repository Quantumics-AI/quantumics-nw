import { Component, OnInit, importProvidersFrom } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { Location } from '@angular/common';
import { RuleCreationService } from '../../services/rule-creation.service';

@Component({
  selector: 'app-edit-rule',
  templateUrl: './edit-rule.component.html',
  styleUrls: ['./edit-rule.component.scss']
})
export class EditRuleComponent implements OnInit {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  public fg: FormGroup;
  public sourceTargetType: boolean;
  public isShowData: boolean = false;
  public projectId: number;
  userId: number;
  public selectedNullName: string;
  public completenessData: any = [];
  public ruleTypesData: any;
  public selectedMainRuleType: string;
  public selectedSubLevel: string;
  invalidPattern: boolean = false;
  invalidCapitalPattern: boolean = false;
  invalidDesPattern: boolean = false;
  public dataConnectionList: any;
  public selectedDataConnections1: string;
  public selectedDataConnections2: string;
  public selectedAttributeName: string;
  public selectedMultipleAttributeName: string;
  public bucketList: any;
  public selectedBucketOne: string;
  public selectedBucketTwo: string;
  public connectionParams: any;
  public bucketSourceOne: any = [];
  public bucketSourceTwo: any = [];
  public saveRulePayload: any;
  public ruleId: number;
  public fetchEditRule: any;
  public ruleFilter = [
    { label: 'Active', name: 'Active', selected: false },
    { label: 'Inactive', name: 'Inactive', selected: false },
    { label: 'Deleted', name: 'Deleted', selected: false }
  ];

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
    private location: Location,
    private ruleCreationService: RuleCreationService) {
      this.certificate$ = this.quantumFacade.certificate$;
      this.certificate$
        .pipe(takeUntil(this.unsubscribe))
        .subscribe(certificate => {
          if (certificate) {
            this.certificateData = certificate;
            this.userId = +this.certificateData.user_id;
          }
        });

        this.activatedRoute.queryParams.subscribe(params => {
          this.ruleId = +params.dataId;
        });
  }

  ngOnInit(): void {
    const ruleDescriptionPattern = /^[A-Za-z\s!@#$%^&*()\-_+=\[\]{}|;:'",.<>?/]*$/;
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');
    this.fg = this.fb.group({ 
      // '^[A-Za-z\\s!@#$%^&*()\\-_+=\\[\\]{}|;:\'",.<>?/]*$'
      ruleName: ['', [Validators.required, Validators.pattern(/^([A-Z]).([A-Za-z0-9_:-]+\s)*[A-Za-z0-9_:-]+$/)]],
      ruleDescription: ['', [Validators.required, Validators.pattern(ruleDescriptionPattern)]],
      sourceAndTarget: [{ value: '', disabled: true }],
      //source 
      //source1 - source2
      sourceDataSource: new FormControl({ value: 'aws', disabled: true }),
      subDataSourceOne: new FormControl({ value: 's3', disabled: true }),
      sourceDataConnection: new FormControl({ value: '', disabled: true }),
      sourceBucketOne: new FormControl({ value: '', disabled: true }),
      sourceFolderPath: new FormControl({ value: '', disabled: true }),
      //
      sourceDataSourceTwo: new FormControl({ value: 'aws', disabled: true }),
      subDataSourceTwo: new FormControl({ value: 's3', disabled: true }),
      sourceDataConnectionTwo: new FormControl({ value: '', disabled: true }),
      sourceBucketTwo: new FormControl({ value: '', disabled: true }),
      sourceFolderPathTwo: new FormControl({ value: '', disabled: true }),
      //types
      ruleType: new FormControl({ value: '', disabled: true }),
      subLavelRadio: new FormControl({ value: '', disabled: true }),
      selectColumnAttribute: new FormControl({ value: '', disabled: true }),
      selectMultipleAttribute: new FormControl({ value: '', disabled: true }),
      percentage: new FormControl({ value: '', disabled: true }),
      //
      status: new FormControl('', Validators.required)
    });

    this.getDataConnection();
    this.getBucketData();
  }

  ngAfterViewInit(): void {
    this.getSelectedRule();
  }

  public getSelectedRule(): void {
    this.ruleCreationService.getEditRule(this.userId, this.projectId, this.ruleId).subscribe((response) => {
      this.fetchEditRule = response.result;
      
      this.fg.controls.ruleName.setValue(this.fetchEditRule?.ruleName);
      this.fg.controls.ruleDescription.setValue(this.fetchEditRule?.ruleDescription);
      this.fg.controls.sourceAndTarget.setValue(this.fetchEditRule?.sourceAndTarget);
      if(response.result.sourceAndTarget) {
        //source1-
        this.fg.controls.sourceDataConnection.setValue(this.fetchEditRule?.sourceData.dataSourceId);
        this.fg.controls.sourceBucketOne.setValue(this.fetchEditRule?.sourceData.bucketName);
        this.selectedBucketOne = this.fetchEditRule?.sourceData.bucketName;
        this.bucketSourceOne.push(this.fetchEditRule?.sourceData.bucketName);
        this.fg.controls.sourceFolderPath.setValue(this.fetchEditRule?.sourceData.filePath);
        //source-2
        this.selectedDataConnections2 = this.fetchEditRule?.targetData.dataSourceId;
        this.fg.controls.sourceDataConnectionTwo.setValue(this.fetchEditRule?.targetData.dataSourceId);
        this.fg.controls.sourceBucketTwo.setValue(this.fetchEditRule?.targetData.bucketName);
        this.selectedBucketTwo = this.fetchEditRule?.targetData.bucketName;
        this.bucketSourceTwo.push(this.fetchEditRule?.targetData.bucketName);
        this.fg.controls.sourceFolderPathTwo.setValue(this.fetchEditRule?.targetData.filePath);
      } else {
        //source
        this.fg.controls.sourceDataConnection.setValue(this.fetchEditRule?.sourceData.dataSourceId);
        this.fg.controls.sourceBucketOne.setValue(this.fetchEditRule?.sourceData.bucketName);
        this.bucketSourceOne.push(this.fetchEditRule?.sourceData.bucketName);
        this.fg.controls.sourceFolderPath.setValue(this.fetchEditRule?.sourceData.filePath);
      }
      this.getRuleTypeList();
      // status
      this.fg.controls.status.setValue(this.fetchEditRule?.status)
      this.fg.controls.percentage.setValue(this.fetchEditRule?.ruleDetails.ruleLevel.acceptance);
      
      // this.fg.controls.status.setValue(result?.status)
      // this.fg.controls.synonymTerm.setValue(result?.synonymTerm);
      //Types
      setTimeout(() => {
        this.fg.controls.ruleType.setValue(this.fetchEditRule?.ruleDetails.ruleTypeName);
        this.fg.controls.subLavelRadio.setValue(this.fetchEditRule?.ruleDetails.ruleLevel.levelName);
        this.selectedSubLevel = this.fetchEditRule?.ruleDetails.ruleLevel.levelName;
        this.fg.controls.selectColumnAttribute.setValue(this.fetchEditRule?.ruleDetails.ruleLevel.columns[0]);
        this.completenessData.push(this.fetchEditRule?.ruleDetails.ruleLevel.columns[0]);
        this.selectedAttributeName = this.fetchEditRule?.ruleDetails.ruleLevel.columns[0];
      }, 5000); // 5000 milliseconds (5 seconds)
      
    }, (error) => {

    })

    
  }

  public getDataConnection(): void {
    this.ruleCreationService.getDataConnection(this.projectId, this.userId).subscribe((response) => {
      this.dataConnectionList = response;
    }, (error) => {

    })
  }

  public getRuleTypeList(): void {
    this.ruleCreationService.getRuleTypes(this.projectId, !this.sourceTargetType).subscribe((response) => {
      this.ruleTypesData = response.result;
      
    }, (error) => {

    })
  }

  public getBucketData(): void {
    this.ruleCreationService.getBucketList(this.userId,this.projectId).subscribe((response) => {
      this.bucketList = response; 
    }, (error) => {

    });
  }

  public selectedType(type: boolean): void {
    // if (type) {
      
    // } else {

    // }
  }

  public continue(): void {
    this.snakbar.open("Rule updated successfully!");
    this.router.navigate([`projects/${this.projectId}/data-quality/rule-types`]);
  }

  public showData(): void {
    this.isShowData = !this.isShowData;
    if (!this.fg.valid) {
      return;
    }

  }

  public back(): void {
    this.location.back();
  }

  public onChangeRuleTypes(value: string): void {
    this.selectedSubLevel = null;
    this.selectedMainRuleType = value;
    console.log("type:", value);
  }

  public onChangeSubLevel(value: string): void {
    this.selectedSubLevel = value;
  }

  // browse table 
  public browseTable(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/create/table`]);
  }

  modelChangeRuleName(str) {
    // const re = /^[a-zA-Z0-9_]+$/;

    if (this.fg.controls.ruleName.value != "") {

      const validCapital = String(str).match(/^([A-Z])/);
      
      if (validCapital == null) {
        this.invalidCapitalPattern = true;
      } else {
        this.invalidCapitalPattern = false;
        const validWorkSpaceName = String(str)
        .match(
          /^([A-Z]).([A-Za-z0-9_:-]+\s)*[A-Za-z0-9_:-]+$/
        );

        if (validWorkSpaceName == null) {
          this.invalidPattern = true;

        } else {
          this.invalidPattern = false;
        }
      }
    } else {
      this.invalidPattern = false;
      this.invalidCapitalPattern = false;
    }

  }

  modelChangeDescription(str) {
    if(this.fg.controls.ruleDescription.value != ""){
      const validPattern = String(str)
      .match(
        /^[A-Za-z\s!@#$%^&*()\-_+=\[\]{}|;:'",.<>?/]*$/
      );

      if (validPattern == null) {
        this.invalidDesPattern = true;

      } else {
        this.invalidDesPattern = false;
      }
    } else {
      this.invalidDesPattern = false;
    }
  }

  public onSelectDataConnections1(d: string): void {
    const connectionBucket = this.dataConnectionList.find(e => e.id == +d);
    this.bucketSourceOne = []
    const arr = [];
    arr.push(connectionBucket.bucketName);
    this.bucketSourceOne.push(connectionBucket.bucketName);
  }

  public onSelectDataConnections2(d: string): void {
    const connectionBucket = this.dataConnectionList.find(e => e.id == +d);
    this.bucketSourceTwo = [];
    const arr = [];
    arr.push(connectionBucket.bucketName);
    this.bucketSourceTwo.push(connectionBucket.bucketName);
  }

  public onSelectBucketOne(d: string): void {

  }

  public onSelectBucketTwo(d: string): void {

  }

  public updateRuleFunction(): void {
    if (this.fg.controls.sourceAndTarget.value) {
      this.saveRulePayload = {
        ruleId: this.ruleId,
        ruleName:this.fg.controls.ruleName.value,
        ruleDescription:this.fg.controls.ruleDescription.value,
        sourceAndTarget:this.fg.controls.sourceAndTarget.value,
        sourceData : {
            sourceDataType: this.fg.controls.sourceDataSource.value,
            subDataSourceType: this.fg.controls.subDataSourceOne.value,
            dataSourceId: +this.fg.controls.sourceDataConnection.value,
            bucketName: this.fg.controls.sourceBucketOne.value,
            filePath : this.fg.controls.sourceFolderPath.value
        },
        targetData: {
          sourceDataType: this.fg.controls.sourceDataSourceTwo.value,
          subDataSourceType: this.fg.controls.subDataSourceTwo.value,
          dataSourceId: +this.fg.controls.sourceDataConnectionTwo.value,
          bucketName: this.fg.controls.sourceBucketTwo.value,
          filePath : this.fg.controls.sourceFolderPathTwo.value
        },
        ruleDetails:{
            ruleTypeName : this.fg.controls.ruleType.value,
            ruleLevel :{
              levelName: this.fg.controls.subLavelRadio.value,
              columnLevel: false,
              acceptance: this.fg.controls.percentage.value,
              columns: [this.fg.controls.selectColumnAttribute.value]
            }
    
        },
        userId : this.userId,
        status: this.fg.controls.status.value   
      }
      // console.log("Rule save payload:", JSON.stringify(req));
    } else {
      this.saveRulePayload = {
        ruleId: this.ruleId,
        ruleName:this.fg.controls.ruleName.value,
        ruleDescription:this.fg.controls.ruleDescription.value,
        sourceAndTarget:this.fg.controls.sourceAndTarget.value,
        sourceData : {
            sourceDataType: this.fg.controls.sourceDataSource.value,
            subDataSourceType: this.fg.controls.subDataSourceOne.value,
            dataSourceId: +this.fg.controls.sourceDataConnection.value,
            bucketName: this.fg.controls.sourceBucketOne.value,
            filePath : this.fg.controls.sourceFolderPath.value
        },
        ruleDetails:{
            ruleTypeName : this.fg.controls.ruleType.value,
            ruleLevel :{
              levelName: this.fg.controls.subLavelRadio.value,
              columnLevel: false,
              acceptance: this.fg.controls.percentage.value,
              columns: [this.fg.controls.selectColumnAttribute.value]
            }
    
        },
        userId : this.userId,
        status: this.fg.controls.status.value   
      }

      // console.log("FALSE -- Rule save payload:", JSON.stringify(req));
    }

    this.ruleCreationService.updateRule(this.userId, this.projectId, this.saveRulePayload).subscribe((response) => {
      if (response?.code === 200) {
        this.snakbar.open(response.message);
        this.router.navigate([`projects/${this.projectId}/data-quality`]);
      } else {
        this.snakbar.open(response.message);
      }
    }, (error) => {
      this.snakbar.open(error);
    });
    // this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }
}
