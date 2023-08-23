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

}
