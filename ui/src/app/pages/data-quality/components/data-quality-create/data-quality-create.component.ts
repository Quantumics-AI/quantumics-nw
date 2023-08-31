import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { Location } from '@angular/common';

@Component({
  selector: 'app-data-quality-create',
  templateUrl: './data-quality-create.component.html',
  styleUrls: ['./data-quality-create.component.scss']
})
export class DataQualityCreateComponent implements OnInit {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  public fg: FormGroup;
  public sourceTargetType: string = 'sourceAndTarget';
  public isShowData: boolean = false;
  public projectId: number;

  public queryRuleTypes: string;
  public queryDcLevel: string;
  public queryDpLevel: string;
  public queryDvLevel: string;

  public showDcRow: boolean;
  public showDCCL: boolean;
  public showDcRowLevel: boolean;
  public showDcColumnLevel: boolean;

  public showDpRow: boolean;
  public showDPCL: boolean;
  public showDpRowLevel: boolean;
  public showDpColumnLevel: boolean;

  public showNull: boolean;

  public showDvRow: boolean;
  public selectColumnMultiRadio: boolean;
  public showDVCL: boolean;
  public showDVMulti: boolean;
  public showDupRow: boolean;
  public showDupCol: boolean;
  public showDupMulCol: boolean;

  public selectedCompleteName: string;
  public completenessData: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  public selectedDataProfiler: string;
  public dataProfiler: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  public selectedDuplicateDataOne: string;
  public duplicateDataOne: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  public duplicateDataTwo: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  selectedColumn = [];

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
    private location: Location,) {
      this.certificate$ = this.quantumFacade.certificate$;
      this.certificate$
        .pipe(takeUntil(this.unsubscribe))
        .subscribe(certificate => {
          if (certificate) {
            this.certificateData = certificate;
          }
        });
  }

  ngOnInit(): void {
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');
    this.fg = this.fb.group({
      ruleName: new FormControl('', Validators.required),
      ruleDescription: new FormControl('', Validators.required),
      type: ['sourceAndTarget'],
      sourceDataSource: new FormControl('aws'),
      sourceDataConnection: new FormControl('aws1'),
      sourceBucket: new FormControl('bucket1'),
      sourceFolder: new FormControl('folder1'),

      targetDataSource: new FormControl('aws', Validators.required),
      targetDataConnection: new FormControl('aws1', Validators.required),
      targetBucket: new FormControl('bucket1', Validators.required),
      targetFolder: new FormControl('folder1', Validators.required),
      ruleType: new FormControl(''),
      dcLevel: new FormControl(''),
      relatedAttributes: new FormControl(''),
      dcRowPercentage: new FormControl(''),
      dcColumnPercentage: new FormControl(''),
      dpLevel: new FormControl(''),
      relatedDataProfiler: new FormControl(''),
      dpTablePercentage: new FormControl(''),
      dpColumnPercentage: new FormControl(''),
      nullPercentage: new  FormControl(''),
      dvLevel: new FormControl(''),
      relatedDuplicateDataOne: new FormControl(''),
      relatedDuplicateDataTwo: new FormControl(''),
      dvDuplicatePercentage: new FormControl(''),
      dvColumnPercentage: new FormControl(''),
      dvMultColumnPercentage: new FormControl(''),
    });
  }

  public selectedType(type: string): void {
    if (type === 'sourceAndTarget') {
      this.fg.get('sourceDataSource').setValidators([Validators.required]);
      this.fg.get('sourceDataConnection').setValidators([Validators.required]);
      this.fg.get('sourceBucket').setValidators([Validators.required]);
      this.fg.get('sourceFolder').setValidators([Validators.required]);
    } else {
      this.fg.get('sourceDataSource')?.clearValidators();
      this.fg.get('sourceDataConnection')?.clearValidators();
      this.fg.get('sourceBucket')?.clearValidators();
      this.fg.get('sourceFolder')?.clearValidators();
    }

    this.fg.get('sourceDataSource')?.updateValueAndValidity();
    this.fg.get('sourceDataConnection')?.updateValueAndValidity();
    this.fg.get('sourceBucket')?.updateValueAndValidity();
    this.fg.get('sourceFolder')?.updateValueAndValidity();
  }

  public continue(): void {
    // this.router.navigate([`projects/${this.projectId}/data-quality/rule-types`]);
    this.snakbar.open("Rule saved");
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
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

  public onItemChange(value: string): void {
    switch (value) {
      case value = "dc":
        this.showDpRow = false;
        this.showDPCL = false;
        this.showDCCL = false;
        this.showDpRowLevel = false;
        this.showDpColumnLevel = false;
        this.showDcColumnLevel = false;
        this.showDcRowLevel = false;
        this.showNull = false;
        this.showDvRow = false;
        this.selectColumnMultiRadio = false;
        this.showDupCol = false;
        this.showDupMulCol = false;
        this.showDVMulti = false;
        this.showDVCL = false;
        this.showDupRow = false;
        this.setBlankValue();
        this.showDcRow = true;
        break;
      
      case value = 'dp':
        this.showDcRow = false;
        this.showDCCL = false;
        this.showDcRowLevel = false;
        this.showDcColumnLevel = false;
        this.showNull = false;
        this.showDvRow = false;
        this.showDPCL = false;
        this.showDpRowLevel = false;
        this.showDpColumnLevel = false;
        this.selectColumnMultiRadio = false;
        this.showDupCol = false;
        this.showDupMulCol = false;
        this.showDVMulti = false;
        this.showDVCL = false;
        this.showDupRow = false;
        this.setBlankValue();
        this.showDpRow = true;
        break;
      
      case value = 'nu':
        this.showDcRow = false;
        this.showDCCL = false;
        this.showDcRowLevel = false;
        this.showDcColumnLevel = false;
        this.showDpRow = false;
        this.showDvRow = false;
        this.selectColumnMultiRadio = false;
        this.showDupCol = false;
        this.showDupMulCol = false;
        this.showDVMulti = false;
        this.showDVCL = false;
        this.showDupRow = false;
        this.showDPCL = false;
        this.showDpRowLevel = false;
        this.showDpColumnLevel = false;
        this.setBlankValue();
        this.showNull = true;
        break;
      
      case value = 'dv':
        this.showDcRow = false;
        this.showDCCL = false;
        this.showDcRowLevel = false;
        this.showDcColumnLevel = false;
        this.showDpRow = false;
        this.showNull = false;
        this.showDPCL = false;
        this.showDpRowLevel = false;
        this.showDpColumnLevel = false;
        this.selectColumnMultiRadio = false;
        this.showDupCol = false;
        this.showDupMulCol = false;
        this.showDVMulti = false;
        this.showDVCL = false;
        this.showDupRow = false;
        this.setBlankValue();
        this.showDvRow = true;

      default:
        break;
    }
    
  }

  public setBlankValue(): void {
    this.fg.controls.dcLevel.setValue('');
    this.fg.controls.dpLevel.setValue('');
    this.fg.controls.dvLevel.setValue('');
  }

  // 1st
  public onSubDcRadioChange(value: string): void {
    if (value == 'dccl') {
      this.showDCCL = true;
      this.showDcRowLevel = false;
      this.showDcColumnLevel = true;
    } else {
      this.showDCCL = false;
      this.showDcColumnLevel = false;
      this.showDcRowLevel = true;
    }
  }

  // 2nd
  public onSubDpRadioChange(value: string): void {
    if (value == 'dpcl') {
      this.showDPCL = true;
      this.showDpRowLevel = false;
      this.showDpColumnLevel = true;
    } else {
      this.showDPCL = false;
      this.showDpColumnLevel = false;
      this.showDpRowLevel = true;
    }
  }

  //4th
  public onSubDvRadioChange(value: string): void {
    if (value == 'dvdr') {
      this.selectColumnMultiRadio = false;
      this.showDupCol = false;
      this.showDupMulCol = false;
      this.showDVMulti = false;
      this.showDVCL = false;
      this.showDupRow = true;
    } else if (value == 'dvc') {
      this.showDupMulCol = false;
      this.showDVMulti = false;
      this.showDupRow = false;
      this.selectColumnMultiRadio = true;
      this.showDVCL = true;
      this.showDupCol = true;
    } else {
      this.showDupRow = false;
      this.showDupCol = false;
      this.showDVCL = false;
      this.selectColumnMultiRadio = true;
      this.showDVMulti = true;
      this.showDupMulCol = true;
    }
  }

}
